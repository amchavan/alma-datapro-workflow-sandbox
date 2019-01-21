from setuptools import setup

dependencies = [
    "adapt-mock-icd",
    "adaptmb"
]

setup(name='adapt-mock-qa2',
      version='0.1',
      description='DRAWS Mock AQUA QA2',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/mock', 'adapt/mock/aqua'],
      install_requires=dependencies,
      zip_safe=False)
