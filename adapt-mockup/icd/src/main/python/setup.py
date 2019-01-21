from setuptools import setup

dependencies = [
    "adaptmb"
]

setup(name='adapt-mock-icd',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/mock', 'adapt/mock/messagebus', 'adapt/mock/messagebus/gen'],
      install_requires=dependencies,
      zip_safe=False)
