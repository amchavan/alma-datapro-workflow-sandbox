from setuptools import setup

dependencies = [
    "adaptmb-messages"
]

setup(name='adaptmb',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/messagebus', 'adapt/resources'],
      install_requires=dependencies,
      include_package_data=True,
      zip_safe=False)
